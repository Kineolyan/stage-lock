package io.kineolyan.stagelock.internal;

import io.gitlab.kineolyan.jrepl.ReplApi;
import io.gitlab.kineolyan.jrepl.ReplCode;
import io.gitlab.kineolyan.jrepl.ReplServer;

import java.util.List;

class Repl {

    public static void main(String[] args) {
        ReplServer.main(args);
    }

    @ReplCode
    static void inspectValue() {
        var value = ReplApi.value("my-key").<List<String>>as();
        // ... do something with the value
    }
}
