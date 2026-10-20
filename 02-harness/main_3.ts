import "dotenv/config";
import { createOpenRouter } from "@openrouter/ai-sdk-provider";
import { generateText, stepCountIs } from "ai";
import { tools } from "./tools";
import { initLogFile, logStep } from "./logger";

async function main() {
  const prompt = process.argv.slice(2).join(" ");

  if (!process.env.OPENROUTER_MODEL) {
    throw new Error("OPENROUTER_MODEL not set in .env");
  }
  initLogFile();

  const systemPrompt = "";

  const openrouter = createOpenRouter({
    apiKey: process.env.OPENROUTER_API_KEY,
  });

  const { text } = await generateText({
    model: openrouter(process.env.OPENROUTER_MODEL),
    system: systemPrompt,
    prompt,
    stopWhen: stepCountIs(10),
    tools,
    onStepFinish: logStep,
  });
  console.log(text);
}

main();
