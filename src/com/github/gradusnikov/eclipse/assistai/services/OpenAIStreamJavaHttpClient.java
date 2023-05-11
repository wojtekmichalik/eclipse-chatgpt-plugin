package com.github.gradusnikov.eclipse.assistai.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

import javax.inject.Inject;
import javax.management.RuntimeErrorException;

import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.di.annotations.Creatable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gradusnikov.eclipse.assistai.Activator;
import com.github.gradusnikov.eclipse.assistai.model.ChatMessage;
import com.github.gradusnikov.eclipse.assistai.model.Conversation;
import com.github.gradusnikov.eclipse.assistai.preferences.PreferenceConstants;

/**
 * A Java HTTP client for streaming requests to OpenAI API.
 * This class allows subscribing to responses received from the OpenAI API and processes the chat completions.
 */
@Creatable
public class OpenAIStreamJavaHttpClient
{

    private String API_KEY;
    private String API_URL = "https://api.openai.com/v1/chat/completions";
    private String MODEL;// = "gpt-4";

    private SubmissionPublisher<String> publisher;

    @Inject
    private ILog logger;
    
    public OpenAIStreamJavaHttpClient()
    {
        publisher = new SubmissionPublisher<>();

    }
    /**
     * Subscribes a given Flow.Subscriber to receive String data from OpenAI API responses.
     * @param subscriber the Flow.Subscriber to be subscribed to the publisher
     */
    public void subscribe(Flow.Subscriber<String> subscriber)
    {
        publisher.subscribe(subscriber);
    }
    /**
     * Returns the JSON request body as a String for the given prompt.
     * @param prompt the user input to be included in the request body
     * @return the JSON request body as a String
     */
    private String getRequestBody(Conversation prompt)
    {

        API_KEY = Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.OPENAI_API_KEY);
        MODEL = Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.OPENAI_MODEL_NAME);


        Map<String, Object>       requestBody = new LinkedHashMap<>();
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> systemMessage = new LinkedHashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content",
                "You are an expert in sofware engineering AI plugin to an IDE. "
                + "Your objective is to assist User in writing and analyzing a soruce code. Flollow these guidelines:\n"
                + "1. Just write the code. Do not explain the code unless asked explicitly.\n "
                + "2. Keep explanation consice and accurate\n"
                + "3. Always use Markdown code blocks when providing code examples.\n"
                + "4. Let's work this out in a step by step way to be sure we have the right answer" );
        messages.add(systemMessage);

        for ( ChatMessage message : prompt.messages() )
        {
            Map<String, Object> userMessage = new LinkedHashMap<>();
            userMessage.put("role", message.getRole());
            userMessage.put("content", message.getContent() );
            messages.add(userMessage);
        }

        requestBody.put("model", MODEL);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("stream", true);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString;
        try
        {
            jsonString = objectMapper.writeValueAsString(requestBody);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException( e );
        }

        return jsonString;
    }

	/**
	 * Executes the HTTP request to OpenAI API with the given prompt and processes the responses.
	 * @param prompt the user input to be sent to the OpenAI API
	 * @throws IOException if an I/O error occurs when sending or receiving
	 * @throws InterruptedException if the operation is interrupted
	 */
    public void run( Conversation prompt ) throws IOException, InterruptedException
    {

        HttpClient client = HttpClient.newHttpClient();
        String requestBody = getRequestBody(prompt);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Accept", "text/event-stream")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        logger.info("Sending request to ChatGPT.");
        
        try
        {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200)
            {
                Activator.getDefault().getLog().error("Request failed: " + response);
                throw new IOException("Request failed: " + response);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.startsWith("data:"))
                {
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data))
                    {
                        break;
                    } 
                    else
                    {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode node = mapper.readTree(data).get("choices").get(0).get("delta");
                        if (node.has("content"))
                        {
                            String content = node.get("content").asText();
                            publisher.submit(content);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            publisher.closeExceptionally(e);
            throw e;
        } 
        finally
        {
            publisher.close();
        }

    }

}