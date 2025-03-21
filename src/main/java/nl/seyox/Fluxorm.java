package nl.seyox;

import lombok.Getter;
import nl.seyox.annotations.FluxormTable;
import nl.seyox.models.Model;
import nl.seyox.reflection.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

public class Fluxorm {

    @Getter
    private static FluxormDriver driver;

    public static void setDriver(FluxormDriver driver) {
        Fluxorm.driver = driver;
        driver.connect();
    }

    public static void migrate() {
        try {
            //driver.createDatabase();
            List<Class<?>> aClass = ReflectionHelper.findAllClassesUsingGoogleGuice().stream().filter(Model.class::isAssignableFrom).toList();
            for (Class<?> aClass1 : aClass) {
                //Exclude the Model class
                if (aClass1.equals(Model.class)) {
                    continue;
                }
                Model model = (Model) aClass1.getConstructor().newInstance();
                driver.createTable(model);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}