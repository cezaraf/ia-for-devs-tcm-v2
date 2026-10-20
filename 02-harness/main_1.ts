import "dotenv/config";

async function main () {
    console.log(`## MODELO USADO: \`${process.env.OPENROUTER_MODEL}\`\n`);
    const prompt = "Corrija o arquivo helloWorld.js";
    
    const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
        method: "POST",
        headers: {
            "Authorization": `Bearer ${process.env.OPENROUTER_API_KEY}`,
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            model: process.env.OPENROUTER_MODEL,
            messages: [
                { role: "user", content: prompt }
            ]
        })
    });

    //console.log(response)

    if (!response.ok) {
        throw new Error(`OpenRouter error: ${response.status} ${response.statusText}`);
    }

    const output = await response.json();
    console.log(`\`\`\`json\n${JSON.stringify(output, undefined, "  ")}\n\`\`\``);
    //console.log(output.choices?.[0]?.message?.content);
}

main();
