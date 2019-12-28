package net.smiguel.stellar.support.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.smiguel.stellar.support.util.Util;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;

public class JsonDateISO8601Deserializer extends JsonDeserializer<Calendar> {

    @Override
    public Calendar deserialize(JsonParser jsonParser, DeserializationContext ctx) throws IOException, JsonProcessingException {
        String date = jsonParser.getText();
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(Util.getDateFormatIso8601().parse(date));
            return calendar;

        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
