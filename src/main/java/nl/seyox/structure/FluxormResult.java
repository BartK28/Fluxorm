package nl.seyox.structure;

import nl.seyox.models.Model;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FluxormResult<T extends Model> {

    private List<T> resultSet = new ArrayList<>();

    public FluxormResult(List<T> resultSet) {
        this.resultSet.addAll(resultSet);
    }

    public FluxormResult<T> with(String field) {
        for (T t : resultSet) {
            try {
                Field f = t.getClass().getDeclaredField(field);
                if (f.getType().equals(Model.class)) {
                    Model model = (Model) f.getType().getConstructor().newInstance();
                    f.setAccessible(true);
                    f.set(t, model.where(t.getClass().getSimpleName() + "Id", t.getId()).first());
                } else if (f.getType().equals(List.class)) {
                    ParameterizedType type = (ParameterizedType) f.getGenericType();
                    Class<?> genericType = (Class<?>) type.getActualTypeArguments()[0];
                    Model model = (Model) genericType.getConstructor().newInstance();
                    List<Model> list = new ArrayList<>(model.where(t.getClass().getSimpleName() + "Id", t.getId()).get());
                    f.setAccessible(true);
                    f.set(t, list);
                }
            } catch (NoSuchFieldException | InvocationTargetException | InstantiationException |
                     IllegalAccessException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    public boolean isEmpty() {
        return resultSet.isEmpty();
    }

    public void add(T t) {
        resultSet.add(t);
    }

    public List<T> get() {
        return resultSet;
    }

    public T first() {
        if (isEmpty()) return null;
        return resultSet.getFirst();
    }

    public FluxormResult<T> orderBy(String field) {
        resultSet.sort((t1, t2) -> {
            try {
                Field f1 = t1.getClass().getDeclaredField(field);
                f1.setAccessible(true);
                Object v1 = f1.get(t1);

                Field f2 = t2.getClass().getDeclaredField(field);
                f2.setAccessible(true);
                Object v2 = f2.get(t2);

                if (v1 instanceof Comparable && v2 instanceof Comparable) {
                    return ((Comparable) v1).compareTo(v2);
                } else {
                    return 0; // Niet vergelijkbaar, dus geen sorting
                }
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        });
        return this;
    }

    public FluxormResult<T> orderByDesc(String field) {
        resultSet.sort(Comparator.comparing(t -> {
            try {
                Field f1 = t.getClass().getDeclaredField(field);
                f1.setAccessible(true);
                Object v1 = f1.get(t);

                Field f2 = t.getClass().getDeclaredField(field);
                f2.setAccessible(true);
                Object v2 = f2.get(t);

                if (v1 instanceof Comparable && v2 instanceof Comparable) {
                    return ((Comparable) v2).compareTo(v1);
                } else {
                    return 0; // Niet vergelijkbaar, dus geen sorting
                }
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }).reversed());
        return this;
    }

    public FluxormResult<T> limit(int limit) {
        resultSet.subList(0, limit);
        return this;
    }


}
