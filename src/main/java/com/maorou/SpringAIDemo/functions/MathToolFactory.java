package com.maorou.SpringAIDemo.functions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.core.io.ClassPathResource;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Component
public class MathToolFactory {

    private List<MathOp> ops;

    @PostConstruct
    public void load() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ClassPathResource resource = new ClassPathResource("math-tools.json");
        ops = mapper.readValue(
                resource.getInputStream(),
                new TypeReference<List<MathOp>>(){}
        );
    }
    SpelExpressionParser spelExpressionParser = new SpelExpressionParser();


    record AddRequest(int a, int b) {}
    record MathOp(String name, String description, String expr){}


    public List<ToolCallback> buildTools(){

        List<ToolCallback> tools = new ArrayList<>();

        for(MathOp m: ops){
            ToolCallback tc = FunctionToolCallback
                    .builder(m.name(), (AddRequest req) -> {
                        StandardEvaluationContext ctx = new StandardEvaluationContext();
                        ctx.setVariable("a", req.a());
                        ctx.setVariable("b", req.b());
                        Expression exp = spelExpressionParser.parseExpression(m.expr());
                        return exp.getValue(ctx, Integer.class);
                    })
                    .description(m.description())
                    .inputType(AddRequest.class)
                    .build();
            tools.add(tc);

        }

        return tools;
    }
}
