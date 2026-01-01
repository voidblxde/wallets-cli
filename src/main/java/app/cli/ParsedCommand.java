package app.cli;

import java.util.List;
import java.util.Map;

public record ParsedCommand(String name, List<String> args, Map<String, String> kv) {
    public String arg(int index) {
        return args.get(index);
    }

    public String restFrom(int index) {
        if (index >= args.size()) return "";
        return String.join(" ", args.subList(index, args.size()));
    }
}
