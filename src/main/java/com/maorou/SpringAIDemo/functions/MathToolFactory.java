package com.maorou.SpringAIDemo.functions;

import com.maorou.SpringAIDemo.config.ChatClientConfig;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntBinaryOperator;

@Component
public class MathToolFactory {

    record AddRequest(int a, int b) {}
    record MathOp(String name, String description, IntBinaryOperator op){}

    private final List<MathOp> ops = List.of(
            new MathOp("add",      "Add two integers a and b",      (a, b) -> a + b),
            new MathOp("subtract", "Subtract b from a", (a, b) -> a - b),
            new MathOp("multiply", "Multiply two integers a and b", (a, b) -> a * b),
            new MathOp("power", "Raise a to the power of b", (a, b) -> (int) Math.pow(a, b))
    );





    public List<ToolCallback> buildTools(){

        List<ToolCallback> tools = new ArrayList<>();

        for(MathOp m: ops){
            ToolCallback tc = FunctionToolCallback
                    .builder(m.name(), (AddRequest req) -> m.op().applyAsInt(req.a(), req.b()))
                    .description(m.description())
                    .inputType(AddRequest.class)
                    .build();
            tools.add(tc);
        }

        return tools;
    }
}
