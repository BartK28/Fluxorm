package nl.seyox.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nl.seyox.Fluxorm;
import nl.seyox.annotations.FluxormTable;
import nl.seyox.annotations.JsonColumn;
import nl.seyox.annotations.SkipColumn;
import nl.seyox.structure.DataTypeMap;
import nl.seyox.structure.FluxormColumn;
import nl.seyox.structure.FluxormResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class Model {

    private int id = -1;

    public <T extends Model> FluxormResult<T> all() {
        return (FluxormResult<T>) Fluxorm.getDriver().all(this.getClass());
    }

    public <T extends Model> FluxormResult<T> where(String field, Object value) {
        return where(field, "=", value);
    }

    public <T extends Model> FluxormResult<T> where(String field, String operator, Object value) {
        return (FluxormResult<T>) Fluxorm.getDriver().where(this.getClass(), field, operator, value);
    }

    public String getTableName() {
        String tableName = "";
        for (Annotation aClassAnnotation : getClass().getDeclaredAnnotations()) {
            if (aClassAnnotation instanceof FluxormTable) {
                tableName = ((FluxormTable) aClassAnnotation).name();
            }
        }
        if (tableName.isEmpty()) {
            tableName = getClass().getName();
            tableName = tableName.substring(tableName.lastIndexOf(".") + 1).toLowerCase() + "s";
        }
        return tableName;
    }

    public List<FluxormColumn> getColumns() {
        List<FluxormColumn> columns = new ArrayList<>();
        columns.add(new FluxormColumn("id", "int auto_increment primary key"));
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(SkipColumn.class)) continue;
            if (field.isAnnotationPresent(JsonColumn.class)) {
                columns.add(new FluxormColumn(field.getName(), "text"));
            } else {
                if (field.getType().equals(List.class)) continue;
                if (field.getType().equals(Model.class)) continue;
                columns.add(new FluxormColumn(field.getName(), DataTypeMap.getMap().get(field.getType().getSimpleName())));
            }
        }
        return columns;
    }

    public void save() {
        Fluxorm.getDriver().save(this);
    }

}
