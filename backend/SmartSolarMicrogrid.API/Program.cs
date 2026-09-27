/* Module: Shared startup | Member 4: Register map, road route, operator reservation and QR verification services. */
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using SmartSolarMicrogrid.API.Services;
using System.Text;

var builder = WebApplication.CreateBuilder(args);

// Member 4: load only the optional route credential; preserve existing environment/user-secret overrides.
var routesEnvPath = Path.Combine(builder.Environment.ContentRootPath, ".env");
if (string.IsNullOrWhiteSpace(builder.Configuration["GoogleRoutes:ApiKey"]) && File.Exists(routesEnvPath))
{
    var routesKey = DotNetEnv.Env.NoEnvVars().Load(routesEnvPath)
        .LastOrDefault(entry => entry.Key == "GoogleRoutes__ApiKey").Value;
    if (!string.IsNullOrWhiteSpace(routesKey))
        builder.Configuration["GoogleRoutes:ApiKey"] = routesKey;
}

// Add services to the container.
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.Converters.Add(new System.Text.Json.Serialization.JsonStringEnumConverter());
    });
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddOpenApi();

// Register MongoDB Service
builder.Services.AddSingleton<MongoDbService>();
builder.Services.AddScoped<SmartSolarMicrogrid.API.Services.member4.OperatorReservationService>();
builder.Services.AddScoped<SmartSolarMicrogrid.API.Services.member4.HubOperatorReservationService>();
builder.Services.AddScoped<SmartSolarMicrogrid.API.Services.member4.OperatorQrVerificationService>();
builder.Services.AddScoped<SmartSolarMicrogrid.API.Services.member4.StationMapService>();
// Member 4: separate routing credential; existing Maps SDK and station services are unchanged.
builder.Services.AddHttpClient<SmartSolarMicrogrid.API.Services.member4.StationRouteService>(client =>
{
    client.Timeout = TimeSpan.FromSeconds(20);
}).RedactLoggedHeaders(new[] { "X-Goog-Api-Key" });

// Configure CORS
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowViteUI", policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyHeader()
              .AllowAnyMethod();
    });
});

// Configure JWT Authentication
var jwtSettings = builder.Configuration.GetSection("JwtSettings");
var secretKey = jwtSettings["Secret"];
builder.Services.AddAuthentication(options =>
{
    options.DefaultAuthenticateScheme = JwtBearerDefaults.AuthenticationScheme;
    options.DefaultChallengeScheme = JwtBearerDefaults.AuthenticationScheme;
})
.AddJwtBearer(options =>
{
    options.TokenValidationParameters = new TokenValidationParameters
    {
        ValidateIssuer = true,
        ValidateAudience = true,
        ValidateLifetime = true,
        ValidateIssuerSigningKey = true,
        ValidIssuer = jwtSettings["Issuer"],
        ValidAudience = jwtSettings["Audience"],
        IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(secretKey))
    };
});

var app = builder.Build();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}

// app.UseHttpsRedirection();

app.UseCors("AllowViteUI");

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.Run();
