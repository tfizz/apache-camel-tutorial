package dev.coder.apache.camel.route;

import dev.coder.apache.camel.model.Student;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.debezium.DebeziumConstants;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class StudentRoute extends RouteBuilder {
    private final String MONGODB_ENDPOINT = "mongodb:mongo?database=testDB&collection=students";
    private final String INSERT_OPERATION = "&operation=insert";
    private final String UPDATE_OPERATION = "&operation=update";
    private final String DELETE_OPERATION = "&operation=remove";

    @Override
    public void configure() throws Exception {
        from("direct:studentInsert")
                .process(exchange -> {
                    Map body = exchange.getMessage().getBody(Map.class);
                    exchange.getMessage().setBody(Student.builder()
                            .firstName((String) body.get("firstName"))
                            .lastName((String) body.get("lastName"))
                            .email((String) body.get("email"))
                            .build()
                    );
                })
                .log("Insert Event: ${body}")
                .to(MONGODB_ENDPOINT + INSERT_OPERATION);

        from("direct:studentUpdate")
                .log("Update Event: ${body}")
                .process(exchange -> {
                    Map currentState = exchange.getMessage().getBody(Map.class);
                    Map previousState = exchange.getIn().getHeader(DebeziumConstants.HEADER_BEFORE, Map.class);
                    List<Document> outBody = new ArrayList<>(2);
                    Document updates = new Document();
                    updates.put("firstName", currentState.get("firstName"));
                    updates.put("lastName", currentState.get("lastName"));
                    updates.put("email", currentState.get("email"));
                    outBody.add(new Document("email", previousState.get("email")));
                    outBody.add(new Document("$set", updates));
                    exchange.getIn().setBody(outBody);
                })
                .to(MONGODB_ENDPOINT + UPDATE_OPERATION);


        from("direct:studentDelete")
                .log("Delete Event: ${headers.CamelDebeziumBefore}")
                .process(exchange -> {
                    Map previousState = exchange.getIn().getHeader(DebeziumConstants.HEADER_BEFORE, Map.class);
                    exchange.getIn().setBody(new Document("email", previousState.get("email")));
                })
                .to(MONGODB_ENDPOINT + DELETE_OPERATION);
    }
}
