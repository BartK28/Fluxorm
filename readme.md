# Fluxorm #

## Introduction ##
Fluxorm is a database helper similar to Laravel's Eloquent ORM.
It is designed to be easy to use and easy to learn.

## Usage ##
### Setting up the driver ###

```java
FluxormSettings settings = new FluxormSettings("jdbc:mysql://host:port", "database","username","password");
Fluxorm.setDriver(new HikariFluxormDriverImpl(settings));
```
### Migrating the database ###
```java
Fluxorm.migrate();
```
### Creating a model ###

```java
import nl.seyox.annotations.FluxormTable;

@Getter
@Setter
@FluxormTable(
        name = "cars"
)
public class Car extends Model {

    private String name;
    private int mileage;
    
    private List<Driver> drivers;

}
```

```java
@Getter
@Setter
@FluxormTable(
        name = "drivers"
)
public class Driver extends Model {

    private String name;
    private int carId;

}
```

### Saving a model ###
```java
Car car = new Car();
car.setName("BMW");
car.setMileage(10000);
car.save();

Driver driver = new Driver();
driver.setName("Driver1");
driver.setCarId(car.getId());
driver.save();
```

### Getting a model ###
```java
Car car = new Car().where("name", "BMW").with("drivers").first();
for (Driver driver : car.getDrivers()) {
    System.out.println(driver.getName());
}
```

### Editting a model ###
```java
Car car = (Car)new Car().where("name", "BMW").first();
car.setMileage(200000);
car.save();
```