package net.smiguel.stellar.support.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.smiguel.stellar.support.serializer.JsonDateTimeISO8601Deserializer;
import net.smiguel.stellar.support.serializer.JsonDateTimeISO8601Serializer;

import java.util.Calendar;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class BaseModel {
    private Long id;

    @JsonSerialize(using = JsonDateTimeISO8601Serializer.class)
    @JsonDeserialize(using = JsonDateTimeISO8601Deserializer.class)
    @JsonProperty(value = "insert_date")
    private Calendar insertDate;

    @JsonProperty(value = "insert_username")
    private String insertUsername;

    @JsonSerialize(using = JsonDateTimeISO8601Serializer.class)
    @JsonDeserialize(using = JsonDateTimeISO8601Deserializer.class)
    @JsonProperty(value = "update_date")
    private Calendar updateDate;

    @JsonProperty(value = "update_username")
    private String updateUsername;

    private boolean active;
}
