const statusEl = document.getElementById("status");
const exportsEl = document.getElementById("exports");

const addOneValue = document.getElementById("addone-value");
const addOneRun = document.getElementById("addone-run");
const addOneResult = document.getElementById("addone-result");

const maxLeft = document.getElementById("max-left");
const maxRight = document.getElementById("max-right");
const maxRun = document.getElementById("max-run");
const maxResult = document.getElementById("max-result");

const sumLimit = document.getElementById("sum-limit");
const sumRun = document.getElementById("sum-run");
const sumResult = document.getElementById("sum-result");

const counterStart = document.getElementById("counter-start");
const counterStep = document.getElementById("counter-step");
const counterRun = document.getElementById("counter-run");
const counterResult = document.getElementById("counter-result");

let wasmExports = null;
let wasmMemory = null;
let heapOffset = 1024;

function readInt(input) {
  return Number.parseInt(input.value, 10) || 0;
}

function setStatus(message) {
  statusEl.textContent = message;
}

function setExportList(exportsObject) {
  const functionNames = Object.keys(exportsObject)
    .filter((name) => typeof exportsObject[name] === "function")
    .sort();

  exportsEl.innerHTML = functionNames
    .map((name) => `<code>${name}</code>`)
    .join("");
}

function enableButtons() {
  addOneRun.disabled = false;
  maxRun.disabled = false;
  sumRun.disabled = false;
  counterRun.disabled = false;
}

function alignToEight(value) {
  return (value + 7) & ~7;
}

function malloc(size) {
  const byteSize = Number(size);
  const start = alignToEight(heapOffset);
  heapOffset = start + byteSize;

  if (wasmMemory) {
    const pageSize = 65536;
    const needed = heapOffset - wasmMemory.buffer.byteLength;
    if (needed > 0) {
      wasmMemory.grow(Math.ceil(needed / pageSize));
    }
  }

  return start;
}

async function loadModule() {
  try {
    const response = await fetch("./demo.wasm");
    if (!response.ok) {
      throw new Error(`fetch failed with ${response.status}`);
    }

    const bytes = await response.arrayBuffer();
    const { instance } = await WebAssembly.instantiate(bytes, {
      env: { malloc }
    });

    const requiredExports = ["AddOne", "MaxValue", "SumToN", "CounterDemo"];
    for (const name of requiredExports) {
      if (typeof instance.exports[name] !== "function") {
        throw new Error(`${name} export was not found in demo.wasm`);
      }
    }

    wasmExports = instance.exports;
    wasmMemory = instance.exports.memory;
    enableButtons();
    setExportList(instance.exports);
    setStatus("module loaded");
  } catch (error) {
    setStatus(`module load failed\n\n${error.message}\n\nRun 'bash build_wasm_demo.sh' and refresh this page.`);
  }
}

addOneRun.addEventListener("click", () => {
  const value = readInt(addOneValue);
  addOneResult.textContent = `AddOne(${value}) = ${wasmExports.AddOne(value)}`;
});

maxRun.addEventListener("click", () => {
  const left = readInt(maxLeft);
  const right = readInt(maxRight);
  maxResult.textContent = `MaxValue(${left}, ${right}) = ${wasmExports.MaxValue(left, right)}`;
});

sumRun.addEventListener("click", () => {
  const limit = readInt(sumLimit);
  sumResult.textContent = `SumToN(${limit}) = ${wasmExports.SumToN(limit)}`;
});

counterRun.addEventListener("click", () => {
  const start = readInt(counterStart);
  const step = readInt(counterStep);
  counterResult.textContent = `CounterDemo(${start}, ${step}) = ${wasmExports.CounterDemo(start, step)}`;
});

loadModule();
