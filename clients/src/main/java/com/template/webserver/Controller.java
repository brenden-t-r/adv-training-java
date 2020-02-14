package com.template.webserver;

import com.google.common.collect.ImmutableList;
import com.template.states.IOUCustomSchema;
import com.template.states.IOUState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.*;
import net.corda.core.node.services.vault.QueryCriteria.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static net.corda.core.node.services.vault.QueryCriteriaUtils.getField;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private final CordaRPCOps proxy;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    public Controller(CordaRPCOps proxy) {
        this.proxy = proxy;
    }

    @GetMapping(value = "/getIOUs")
    public List<StateAndRef<IOUState>> getIOUs() {
        return null;
    }

    @GetMapping(value = "/getIOUs/linearId/{linearId}")
    public List<StateAndRef<IOUState>> getIousWithLinearId(@PathVariable String linearId) {
        return null;
    }

    @GetMapping(value = "/getIOUs/greaterThan/{amount}")
    public List<StateAndRef<IOUState>> getIOUsWithAmountGreaterThan(@PathVariable Long amount) throws NoSuchFieldException {
        return null;
    }

}
