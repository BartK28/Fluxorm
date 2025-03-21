# 🚀 Fluxorm - Easy & Elegant ORM for Java

Fluxorm is an intuitive database helper inspired by Laravel's Eloquent ORM. Designed for simplicity, ease of learning, and speed of development.

---

## ⚙️ Quick Setup

### 🔧 Configure the Database Driver

```java
FluxormSettings settings = new FluxormSettings(
    "jdbc:mysql://host:port", 
    "database",
    "username",
    "password"
);

Fluxorm.setDriver(new HikariFluxormDriverImpl(settings));
```

### 🚦 Run Migrations

```java
Fluxorm.migrate();
```

---

## 🗂️ Defining Models

### 🚗 Car Model

```java
@Getter
@Setter
@FluxormTable(name = "cars")
public class Car extends Model {
    private String name;
    private int mileage;
    private List<Driver> drivers;
}
```

### 🧑‍✈️ Driver Model

```java
@Getter
@Setter
@FluxormTable(name = "drivers")
public class Driver extends Model {
    private String name;
    private int carId;
}
```

---

## ✨ Basic Usage

### 💾 Saving Data

```java
Car car = new Car();
car.setName("BMW");
car.setMileage(10000);
car.save();

Driver driver = new Driver();
driver.setName("John Doe");
driver.setCarId(car.getId());
driver.save();
```

### 📖 Retrieving Data

```java
Car car = new Car()
            .where("name", "BMW")
            .with("drivers")
            .first();

for (Driver driver : car.getDrivers()) {
    System.out.println(driver.getName());
}
```

### ✏️ Updating Data

```java
Car car = (Car) new Car().where("name", "BMW").first();
car.setMileage(200000);
car.save();
```

---

Enjoy smooth and productive Java development with Fluxorm! 🎉📦
