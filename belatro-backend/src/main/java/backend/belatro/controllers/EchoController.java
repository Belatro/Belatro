package backend.belatro.controllers;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class EchoController {

    @MessageMapping("/echo")
    @SendTo("/topic/test")
    public String echo(String msg) {
        return msg;
    }
}
