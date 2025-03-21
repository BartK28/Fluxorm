package nl.seyox;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FluxormSettings {

    private String url;
    private String database;
    private String username;
    private String password;

}
