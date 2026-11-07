package com.test.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.test.agent.SqlAgentChatService;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AiControllerTest {

    @Test
    void returnsModelResult() throws Exception {
        SqlAgentChatService client = mock(SqlAgentChatService.class);
        when(client.chat("你好"))
                .thenReturn(
                        java.util.Map.of(
                                "result",
                                "你好！",
                                "observations",
                                java.util.List.of(),
                                "modelRequests",
                                1,
                                "toolCalls",
                                0));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AiController(client)).build();

        mvc.perform(
                        post("/ai/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"prompt\":\"你好\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("你好！"))
                .andExpect(jsonPath("$.toolCalls").value(0))
                .andExpect(jsonPath("$.modelRequests").value(1));
        verify(client).chat("你好");
    }

    @Test
    void rejectsBlankPrompt() throws Exception {
        SqlAgentChatService client = mock(SqlAgentChatService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AiController(client)).build();

        mvc.perform(
                        post("/ai/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"prompt\":\"   \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(client);
    }
}
