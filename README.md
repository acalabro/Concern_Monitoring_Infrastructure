# **CONCERN** - COmplex eveNt proCEssing monitoR iNfrastructure
---

## 🚀 Quick Start with Docker

### Prerequisites
- Docker
- Docker Compose

### Run

```bash
# Clone repository
git clone https://github.com/acalabro/Concern_Monitoring_Infrastructure.git
cd Concern_Monitoring_Infrastructure

# Setup
mkdir -p docker/init-db
chmod +x docker/init-db/init-eventdb.sh

# Build and start
docker-compose build
docker-compose up -d

# Check status
docker-compose ps
```

### Access

- **Dashboard**: http://localhost
- **API**: http://localhost:8181/api/health
- **MySQL**: localhost:3306 (user: `concern`, password: `un53cur3!!`)

### Stop

```bash
docker-compose down
```

---

## 📋 Manual Setup (Development)

### Backend

```bash
cd Concern_Monitoring_Infrastructure
mvn clean compile
mvn exec:java -Dexec.mainClass="it.cnr.isti.labsedc.concern.rest.Main"
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

### Database

```bash
mysql -u concern -pun53cur3!! -e "CREATE DATABASE eventdb;"
mysql -u concern -pun53cur3!! eventdb < docker/init-db/init-eventdb.sh
```

---

## 🛠️ Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `MYSQL_HOST` | `localhost` | MySQL hostname |
| `MYSQL_PORT` | `3306` | MySQL port |
| `MYSQL_USER` | `concern` | MySQL username |
| `MYSQL_PASSWORD` | `un53cur3!!` | MySQL password |
| `MYSQL_DATABASE` | `eventdb` | Database name |

---

## 📊 Features

- Real-time event monitoring
- Complex Event Processing (Drools CEP)
- Event statistics and violations tracking
- Interactive dashboard
- RESTful API
- Rule management (upload, validate, load)

---

## 🐛 Troubleshooting

```bash
# View logs
docker-compose logs -f backend

# Restart services
docker-compose restart

# Reset database (⚠️ deletes data!)
docker-compose down -v
docker-compose up -d
```

---

![Dashboard](https://github.com/acalabro/Concern_Monitoring_Infrastructure/blob/master/1.jpg?raw=true)
![Statistics](https://github.com/acalabro/Concern_Monitoring_Infrastructure/blob/master/2.jpg?raw=true)


## 📝 License

GPL 3.0

## 👥 Contributors

- Antonello Calabrò - [Google Scholar](https://scholar.google.com/citations?user=jfxosJMAAAAJ&hl=it)
