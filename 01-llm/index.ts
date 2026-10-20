import { OpenRouter } from '@openrouter/sdk';
import * as dotenv from 'dotenv';

dotenv.config();

const openRouter = new OpenRouter({
  apiKey: process.env.OPENROUTER_API_KEY || "",
});

const MODEL = 'openai/gpt-oss-20b';

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

async function streamRealtimeTokens() {
  const prompt = "Explique brevemente o que é um token em IA.";

  console.log(`[PROMPT]: "${prompt}"\n`);
  console.log("--- Aguardando resposta da LLM real (Exibindo token por token) ---");

  const stream = await openRouter.chat.send({
    chatRequest: {
      model: MODEL,
      messages: [{ role: 'user', content: prompt }],
      stream: true, // Define que o retorno será no format SSE possibilitando interceptar cada token de output individualmente
    },
  });

  // Iterando sobre o stream à medida que os pacotes chegam da API
  for await (const chunk of stream) {
    const token = chunk.choices?.[0]?.delta?.content;

    if (token) {
      process.stdout.write(token);
      await sleep(500); // Simula um pequeno delay para melhor visualização do fluxo de tokens
    }
  }

  console.log("\n\n--- Fim do fluxo de tokens ---");
  return;
}

streamRealtimeTokens();
