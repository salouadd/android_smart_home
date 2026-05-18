package com.example.smarthome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AiAssistantFragment extends Fragment {

    private RecyclerView rvChat;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private EditText etMessage;
    private FloatingActionButton btnSend;
    private ImageButton btnVoice;
    private GroqService groqService;

    private static final String API_KEY = "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";

    private final ActivityResultLauncher<Intent> speechResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenText = matches.get(0);
                        etMessage.setText(spokenText);
                        sendMessage();
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai_assistant, container, false);

        rvChat = view.findViewById(R.id.rv_chat);
        etMessage = view.findViewById(R.id.et_message);
        btnSend = view.findViewById(R.id.btn_send);
        btnVoice = view.findViewById(R.id.btn_voice);

        // Initialisation de la liste des messages
        messageList = new ArrayList<>();
        messageList.add(new ChatMessage("Bonjour ! Je suis votre assistant SmartHome. Comment puis-je vous aider ?", ChatMessage.TYPE_ASSISTANT));
        
        chatAdapter = new ChatAdapter(messageList);
        rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChat.setAdapter(chatAdapter);

        setupRetrofit();

        // Logique pour basculer entre micro et envoi
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    btnSend.setVisibility(View.VISIBLE);
                    btnVoice.setVisibility(View.GONE);
                } else {
                    btnSend.setVisibility(View.GONE);
                    btnVoice.setVisibility(View.VISIBLE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Le bouton vocal est visible par défaut, le bouton envoi caché
        btnSend.setVisibility(View.GONE);

        btnSend.setOnClickListener(v -> sendMessage());
        btnVoice.setOnClickListener(v -> startVoiceRecognition());

        return view;
    }

    private void setupRetrofit() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(logging).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.groq.com/openai/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        groqService = retrofit.create(GroqService.class);
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        try {
            speechResultLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Vocal non disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        addMessage(new ChatMessage(text, ChatMessage.TYPE_USER));
        etMessage.setText("");

        List<GroqRequest.Message> messages = new ArrayList<>();
        messages.add(new GroqRequest.Message("system", "Tu es un assistant Smart Home."));
        messages.add(new GroqRequest.Message("user", text));

        groqService.getChatCompletion("Bearer " + API_KEY, new GroqRequest("llama-3.1-8b-instant", messages))
                .enqueue(new Callback<GroqResponse>() {
            @Override
            public void onResponse(Call<GroqResponse> call, Response<GroqResponse> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    String reply = response.body().choices.get(0).message.content;
                    addMessage(new ChatMessage(reply, ChatMessage.TYPE_ASSISTANT));
                }
            }
            @Override
            public void onFailure(Call<GroqResponse> call, Throwable t) {
                if (isAdded()) addMessage(new ChatMessage("Erreur de connexion.", ChatMessage.TYPE_ASSISTANT));
            }
        });
    }

    private void addMessage(ChatMessage message) {
        messageList.add(message);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        rvChat.smoothScrollToPosition(messageList.size() - 1);
    }
}
