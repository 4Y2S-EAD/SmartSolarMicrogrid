using System.Collections.Generic;
using Microsoft.Extensions.Configuration;
using SmartSolarMicrogrid.API.Services;
using Xunit;

// Disable parallel test execution to prevent socket exhaustion and timeouts on MongoDB Atlas
[assembly: CollectionBehavior(DisableTestParallelization = true)]

namespace SmartSolarMicrogrid.Tests
{
    public static class TestSharedContext
    {
        public static IConfiguration Configuration { get; }
        public static MongoDbService MongoDbService { get; }

        static TestSharedContext()
        {
            var inMemorySettings = new Dictionary<string, string?>
            {
                { "MongoDbSettings:ConnectionString", "mongodb+srv://nerandadilhara_db_user:C1U79LkKZApQzeVq@cluster0.nac5zjh.mongodb.net/?appName=Cluster0&retryWrites=true&w=majority" },
                { "MongoDbSettings:DatabaseName", "SolarMicrogridDB" },
                { "JwtSettings:Secret", "SuperSecretKeyThatIsAtLeast32BytesLongForJWTAuthentication1234!!" },
                { "JwtSettings:Issuer", "SmartSolarMicrogrid" },
                { "JwtSettings:Audience", "SmartSolarMicrogridUI" },
                { "JwtSettings:ExpiryMinutes", "1440" }
            };

            Configuration = new ConfigurationBuilder()
                .AddInMemoryCollection(inMemorySettings)
                .Build();

            MongoDbService = new MongoDbService(Configuration);
        }
    }
}
