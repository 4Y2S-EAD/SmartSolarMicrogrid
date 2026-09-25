"""Member 4: read-only API contract checks against a running local test API.
Uses locally configured signing credentials only in memory to exercise both existing JWT role formats.
Run from the repository root. No tokens or reservation personal data are printed.
"""
import re
import base64, hashlib, hmac, json, time, urllib.request, urllib.error, urllib.parse
from pathlib import Path

BASE = "http://127.0.0.1:5235/api"

def token(role, mobile=False):
    settings = json.loads(re.sub(r",\s*}", "}", re.sub(r"(?m)^\s*//.*$", "", Path("backend/SmartSolarMicrogrid.API/appsettings.json").read_text())))["JwtSettings"]
    enc = lambda v: base64.urlsafe_b64encode(json.dumps(v,separators=(",",":")).encode()).rstrip(b"=")
    claim = "http://schemas.microsoft.com/ws/2008/06/identity/claims/role" if mobile else "role"
    body = enc({"alg":"HS256","typ":"JWT"})+b"."+enc({"iss":settings["Issuer"],"aud":settings["Audience"],"sub":"operator-verification",claim:role,"exp":int(time.time())+7200})
    return (body+b"."+base64.urlsafe_b64encode(hmac.new(settings["Secret"].encode(),body,hashlib.sha256).digest()).rstrip(b"=")).decode()

def request(path, bearer=None, method="GET", payload=None):
    headers = {"Content-Type":"application/json"}
    if bearer: headers["Authorization"] = "Bearer " + bearer
    req = urllib.request.Request(BASE+path,headers=headers,method=method,data=json.dumps(payload).encode() if payload is not None else None)
    try:
        with urllib.request.urlopen(req,timeout=45) as r: return r.status,json.load(r)
    except urllib.error.HTTPError as e:
        data=e.read()
        return e.code,json.loads(data) if data else None

def run():
    auth=token("gridoperator")
    for suffix in ["", "/pending", "/history", "/search"]:
        assert request("/operator/reservations"+suffix)[0]==401
        assert request("/operator/reservations"+suffix,token("prosumer"))[0]==403
        code,data=request("/operator/reservations"+suffix,auth)
        assert code==200,(suffix,code)
        assert data["summary"]["activeCount"]==data["summary"]["pendingCount"]+data["summary"]["approvedCount"]
        if suffix=="/pending": assert all(x["status"]=="Pending" and x["canApprove"] for x in data["items"])
        if suffix=="/history": assert all(x["status"] in ["Completed","Cancelled"] and not x["canApprove"] for x in data["items"])
    assert request("/operator/reservations",token("GridOperator",True))[0]==200
    for query in ["reservationId=bad", "bookingDate=2026-02-30", "status=99", "status=unknown", "page=0", "pageSize=101", "bookingDate=9999-12-31"]:
        assert request("/operator/reservations/search?"+query,auth)[0]==400,query
    _,data=request("/operator/reservations?pageSize=1",auth)
    assert len(data["items"])<=1
    if data["items"]:
        row=data["items"][0]
        filters={"reservationId":row["reservationId"],"prosumerNic":row["prosumerNic"],"station":row["stationId"],"bookingDate":row["bookingDate"][:10],"status":row["status"]}
        for params in [{k:v} for k,v in filters.items()]+[filters]:
            code,result=request("/operator/reservations/search?"+urllib.parse.urlencode(params),auth)
            assert code==200 and result["totalRecords"]>=1,params.keys()
            assert all(all(str(x[k])[:10]==v if k=="bookingDate" else x[k]==v for k,v in params.items() if k!="station") for x in result["items"])
        if row["stationName"]:
            _,result=request("/operator/reservations/search?station="+urllib.parse.quote(row["stationName"].swapcase()),auth)
            assert result["totalRecords"]>=1
    _,empty=request("/operator/reservations/search?prosumerNic=NO-MATCH-.*",auth)
    assert empty["items"]==[] and empty["totalRecords"]==0
    print("PASS: four GET APIs, web/mobile role claims, 401/403, summary counts, pagination, each search field, combined filters, literal text and invalid queries.")

if __name__=="__main__": run()
