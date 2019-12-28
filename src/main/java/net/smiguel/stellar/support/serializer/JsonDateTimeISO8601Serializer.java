package net.smiguel.stellar.support.serializer;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.smiguel.stellar.support.util.Util;

import java.io.IOException;
import java.util.Calendar;

public class JsonDateTimeISO8601Serializer extends JsonSerializer<Calendar> {

    @Override
    public void serialize(Calendar date, com.fasterxml.jackson.core.JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(Util.getDateTimeFormatIso8601().format(date.getTime()));
    }
}
