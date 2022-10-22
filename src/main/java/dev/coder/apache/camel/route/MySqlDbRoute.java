package dev.coder.apache.camel.route;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.debezium.DebeziumConstants;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class MySqlDbRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("debezium-mysql:db-connector")
                .choice()
                .when(header(DebeziumConstants.HEADER_IDENTIFIER).isEqualTo("mysql-server-01.testDB.students"))
                    .choice()
                        .when(header(DebeziumConstants.HEADER_OPERATION).isEqualTo("c")).to("direct:studentInsert")
                        .when(header(DebeziumConstants.HEADER_OPERATION).isEqualTo("u")).to("direct:studentUpdate")
                        .when(header(DebeziumConstants.HEADER_OPERATION).isEqualTo("d")).to("direct:studentDelete")
                        .otherwise().to("direct:other");

        from("direct:other")
                .log("Received '${header.CamelDebeziumOperation}' Event: ${body}");

    }
}
