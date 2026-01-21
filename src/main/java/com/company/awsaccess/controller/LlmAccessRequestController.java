package com.company.awsaccess.controller;

import com.company.awsaccess.orchestrator.AccessRequestOrchestrator;
import com.company.awsaccess.llm.dto.LlmInterpretRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/llm-access-requests")
public class LlmAccessRequestController {

    private final AccessRequestOrchestrator orchestrator;

    public LlmAccessRequestController(AccessRequestOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/interpret")
    public Object interpret(@RequestBody LlmInterpretRequest request) {
        return orchestrator.handleInitialRequest(request);
    }
}
