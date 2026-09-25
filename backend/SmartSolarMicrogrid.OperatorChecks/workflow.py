"""Member 4 integration fixture. Creates only temporary reservations through existing APIs.
Usage: python .../workflow.py create|verify|cleanup
Cleanup deletes only the IDs recorded by create; no existing reservations are changed.
"""
from verify_http import request, token
from pathlib import Path
import datetime, json, sys, urllib.parse
FILE=Path("backend/SmartSolarMicrogrid.API/bin/operator-verification/test-fixture.json")
auth=token("gridoperator")
def create():
    assert not FILE.exists(), "Clean up the previous test fixture before creating another."
    _,users=request("/prosumers")
    user=next(u for u in users if u["status"]=="active")
    _,stations=request("/stations")
    station=slot=None
    for candidate in stations:
        code,slots=request("/stations/"+candidate["stationId"]+"/slots")
        if code==200 and slots: station=candidate; slot=slots[0]; break
    assert station and slot, "An existing station and slot are required."
    day=(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(days=2)).strftime("%Y-%m-%d")
    records=[]
    FILE.parent.mkdir(parents=True,exist_ok=True)
    for i in range(3):
        payload={"prosumerNic":user["nic"],"stationId":station["stationId"],"slotId":slot["slotId"],"bookingDate":day,"startTime":f"10:{41+i:02} AM","endTime":f"10:{42+i:02} AM"}
        code,row=request("/reservations",auth,"POST",payload)
        assert code==201,(code,"Unable to create test reservation; existing records untouched")
        records.append(row)
        FILE.write_text(json.dumps(records))
    print("Created 3 temporary real API reservations for web, Android and workflow checks.")
def verify():
    records=json.loads(FILE.read_text())
    row=records[2]; rid=row["reservationId"]
    _,before=request("/operator/reservations",auth)
    _,pending=request("/operator/reservations/pending?reservationId="+rid,auth)
    assert len(pending["items"])==1
    assert request("/reservations/"+rid+"/approve",auth,"PUT")[0]==200
    _,after=request("/operator/reservations",auth)
    assert after["summary"]["pendingCount"]==before["summary"]["pendingCount"]-1
    assert after["summary"]["approvedCount"]==before["summary"]["approvedCount"]+1
    assert request("/reservations/"+rid+"/approve",auth,"PUT")[0]==400
    assert request("/reservations/"+rid+"/cancel",auth,"PUT",{"cancellationReason":"Member 4 integration verification"})[0]==200
    _,history=request("/operator/reservations/history?reservationId="+rid,auth)
    assert len(history["items"])==1 and history["items"][0]["status"]=="Cancelled"
    print("PASS: real POST -> Pending -> existing PUT approval -> counts refresh -> duplicate approval rejected -> existing cancellation -> history.")
def cleanup():
    if not FILE.exists(): return
    for row in json.loads(FILE.read_text()):
        assert request("/reservations/"+row["reservationId"],auth,"DELETE")[0] in (200,404)
    FILE.unlink()
    print("Removed only the temporary reservations created by this verification.")
if __name__=="__main__": {"create":create,"verify":verify,"cleanup":cleanup}[sys.argv[1]]()
