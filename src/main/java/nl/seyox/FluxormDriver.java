package nl.seyox;

import nl.seyox.models.Model;
import nl.seyox.structure.FluxormResult;

public interface FluxormDriver {

    void connect();

    void disconnect();

    void createTable(Model model);

    void createDatabase();

    <T extends Model> FluxormResult<T> where(Class<T> model, String field, String operator, Object value);

    <T extends Model> FluxormResult<T> all(Class<T> model);

    void save(Model model);
}
