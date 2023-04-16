package io.kineolyan.stagelock.internal;

import io.gitlab.kineolyan.jrepl.ReplApi;
import io.gitlab.kineolyan.jrepl.ReplCode;
import io.gitlab.kineolyan.jrepl.ReplServer;

import java.util.List;

class Repl {

    public static void main(String[] args) {
        var effectiveArgs = args.length == 0
                ? new String[] {"7777"}
                : args;
        ReplServer.main(effectiveArgs);
    }

    @ReplCode
    static void inspectValue() {
        var value = ReplApi.value("my-key").<List<String>>as();
        // ... do something with the value
    }
}
