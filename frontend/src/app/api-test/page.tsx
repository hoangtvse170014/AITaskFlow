"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";

export default function TestPage() {
  const [result, setResult] = useState<string>("");

  const testLogin = async () => {
    try {
      const response = await fetch("https://taskflow-backend-production-f828.up.railway.app/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: "final_e2e_2026@test.com", password: "Test123456" }),
      });
      const data = await response.json();
      setResult(JSON.stringify(data, null, 2));
    } catch (e: any) {
      setResult("ERROR: " + e.message);
    }
  };

  return (
    <div className="p-8">
      <h1>API Test</h1>
      <Button onClick={testLogin}>Test Login API</Button>
      <pre className="mt-4 p-4 bg-gray-100 rounded overflow-auto">{result}</pre>
    </div>
  );
}
