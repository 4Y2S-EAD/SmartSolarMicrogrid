using Microsoft.Extensions.Configuration;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.Models;

namespace SmartSolarMicrogrid.API.Services
{
    public class MongoDbService
    {
        private readonly IMongoDatabase _database;

        public MongoDbService(IConfiguration configuration)
        {
            DotNetEnv.Env.Load();
            
            var connectionString = Environment.GetEnvironmentVariable("MONGODB_CONNECTION_STRING");
            var databaseName = Environment.GetEnvironmentVariable("MONGODB_DATABASE_NAME");
            
            // Fallback to appsettings if not found in .env
            if (string.IsNullOrEmpty(connectionString) || string.IsNullOrEmpty(databaseName))
            {
                var mongoDbSettings = configuration.GetSection("MongoDbSettings");
                connectionString = mongoDbSettings["ConnectionString"];
                databaseName = mongoDbSettings["DatabaseName"];
            }

            var client = new MongoClient(connectionString);
            _database = client.GetDatabase(databaseName);
        }

        public IMongoCollection<User> Users => _database.GetCollection<User>("Users");
        public IMongoCollection<SolarStationInfo> SolarStations => _database.GetCollection<SolarStationInfo>("SolarStationInfo");
        public IMongoCollection<EnergyBookingSlots> EnergyBookingSlots => _database.GetCollection<EnergyBookingSlots>("EnergyBookingSlots");
        public IMongoCollection<EnergyReservation> EnergyReservations => _database.GetCollection<EnergyReservation>("EnergyReservations");
    }
}
