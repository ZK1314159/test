package com.test.controller;

import com.test.service.direct.DeepSeekClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeepSeekControllerTest {

    @Test
    void returnsModelResult() throws Exception {
        DeepSeekClient client = mock(DeepSeekClient.class);
        when(client.complete("你好")).thenReturn("你好！");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new DeepSeekController(client)).build();

        mvc.perform(post("/deepseek/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"你好\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("你好！"));
        verify(client).complete("你好");
    }

    @Test
    void rejectsBlankPrompt() throws Exception {
        DeepSeekClient client = mock(DeepSeekClient.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new DeepSeekController(client)).build();

        mvc.perform(post("/deepseek/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"   \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(client);
    }
}
