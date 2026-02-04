package com.company.awsaccess.llm.client;

import com.company.awsaccess.llm.dto.LlmInterpretRequest;
import com.company.awsaccess.llm.dto.LlmInterpretResponse;

public interface LlmClient {

    LlmInterpretResponse interpret(LlmInterpretRequest request);
}
