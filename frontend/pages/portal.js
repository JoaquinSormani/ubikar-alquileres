const API_INQUILINOS = "http://localhost:8081/api/inquilinos";
const API_PROPIETARIOS = "http://localhost:8081/api/propietarios";

const accessView = document.getElementById("accessView");
const accessForm = document.getElementById("accessForm");
const dniInput = document.getElementById("dniInput");
const accessError = document.getElementById("accessError");

const roleChoiceView = document.getElementById("roleChoiceView");
const chooseInquilinoBtn = document.getElementById("chooseInquilinoBtn");
const choosePropietarioBtn = document.getElementById("choosePropietarioBtn");
const backToAccessBtn = document.getElementById("backToAccessBtn");
let pendingDualMatch = null;

const portalView = document.getElementById("portalView");
const tenantName = document.getElementById("tenantName");
const roleLabel = document.getElementById("roleLabel");
const logoutBtn = document.getElementById("logoutBtn");

const statusHeading = document.getElementById("statusHeading");
const statusAmount = document.getElementById("statusAmount");
const statusPeriod = document.getElementById("statusPeriod");
const statusBadge = document.getElementById("statusBadge");
const statusDue = document.getElementById("statusDue");
const historyHeading = document.getElementById("historyHeading");
const ledgerList = document.getElementById("ledgerList");
const demoNote = document.getElementById("demoNote");

const uploadTriggerBtn = document.getElementById("uploadTriggerBtn");
const downloadTriggerBtn = document.getElementById("downloadTriggerBtn");
const reminderBtn = document.getElementById("reminderBtn");
const uploadModal = document.getElementById("uploadModal");
const cancelUploadBtn = document.getElementById("cancelUploadBtn");
const confirmUploadBtn = document.getElementById("confirmUploadBtn");
const dropzone = document.getElementById("dropzone");
const dropzoneLabel = document.getElementById("dropzoneLabel");
const fileInput = document.getElementById("fileInput");

const toast = document.getElementById("toast");

// --- Datos de demostracion (todavia no hay modelo de Contrato/Pago/Liquidacion en el backend) ---
const DEMO_INQUILINO = {
  heading: "Estado del mes",
  amount: "$185.400",
  period: "Alquiler de marzo 2026",
  paid: false,
  dueLabel: "Vence el <strong>10 de marzo</strong>. Después de esa fecha se aplica punitorio por mora.",
  paidLabel: "Este mes ya está al día.",
  historyHeading: "Historial de recibos",
  ledger: [
    { period: "Febrero 2026", amount: "$180.200", paid: true },
    { period: "Enero 2026", amount: "$180.200", paid: true },
    { period: "Diciembre 2025", amount: "$172.000", paid: true },
    { period: "Noviembre 2025", amount: "$172.000", paid: true },
  ],
};

const DEMO_PROPIETARIO = {
  heading: "Liquidación del mes",
  amount: "$166.860",
  period: "Marzo 2026 · Av. Colón 1234, Depto 3 (comisión UBIKAR 10% ya descontada)",
  paid: true,
  dueLabel: "Liquidado el <strong>12 de marzo</strong>.",
  paidLabel: "Liquidado el <strong>12 de marzo</strong>.",
  historyHeading: "Historial de liquidaciones",
  ledger: [
    { period: "Febrero 2026", amount: "$162.180", paid: true },
    { period: "Enero 2026", amount: "$162.180", paid: true },
    { period: "Diciembre 2025", amount: "$154.800", paid: true },
    { period: "Noviembre 2025", amount: "$154.800", paid: true },
  ],
};

let currentRole = null;

document.querySelectorAll("[data-demo-dni]").forEach((el) => {
  el.addEventListener("click", () => {
    dniInput.value = el.dataset.demoDni;
    dniInput.focus();
  });
});

accessForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const dni = dniInput.value.trim();
  hideError();

  if (!dni) {
    showError("Ingresá tu DNI.");
    return;
  }

  const submitBtn = accessForm.querySelector("button[type=submit]");
  submitBtn.disabled = true;

  try {
    const match = await findPersonaByDni(dni);

    if (!match) {
      showError("No encontramos ese DNI ni como inquilino ni como propietario. Revisá el número o pedile a UBIKAR que te dé de alta.");
      return;
    }

    if (match.dual) {
      showRoleChoice(match);
      return;
    }

    openPortal(match.data, match.role);
  } catch (err) {
    showError("No pudimos conectar con el servidor. ¿Está corriendo el backend en localhost:8081?");
  } finally {
    submitBtn.disabled = false;
  }
});

async function findPersonaByDni(dni) {
  const [inquilinos, propietarios] = await Promise.all([
    fetch(API_INQUILINOS).then((r) => (r.ok ? r.json() : [])),
    fetch(API_PROPIETARIOS).then((r) => (r.ok ? r.json() : [])),
  ]);

  const inquilino = inquilinos.find((i) => i.dni === dni);
  const propietario = propietarios.find((p) => p.dni === dni);

  // La misma persona puede ser inquilino de una propiedad y propietaria de
  // otra al mismo tiempo: no asumimos un rol, se lo preguntamos.
  if (inquilino && propietario) {
    return { dual: true, inquilino, propietario };
  }
  if (inquilino) return { dual: false, data: inquilino, role: "inquilino" };
  if (propietario) return { dual: false, data: propietario, role: "propietario" };
  return null;
}

function showRoleChoice(match) {
  pendingDualMatch = match;
  accessView.hidden = true;
  roleChoiceView.hidden = false;
}

chooseInquilinoBtn.addEventListener("click", () => {
  if (!pendingDualMatch) return;
  openPortal(pendingDualMatch.inquilino, "inquilino");
  roleChoiceView.hidden = true;
});

choosePropietarioBtn.addEventListener("click", () => {
  if (!pendingDualMatch) return;
  openPortal(pendingDualMatch.propietario, "propietario");
  roleChoiceView.hidden = true;
});

backToAccessBtn.addEventListener("click", () => {
  pendingDualMatch = null;
  roleChoiceView.hidden = true;
  accessView.hidden = false;
});

function showError(message) {
  accessError.textContent = message;
  accessError.hidden = false;
}

function hideError() {
  accessError.hidden = true;
}

function openPortal(persona, role) {
  currentRole = role;
  const demo = role === "inquilino" ? DEMO_INQUILINO : DEMO_PROPIETARIO;
  const endpoint = role === "inquilino" ? "/api/inquilinos" : "/api/propietarios";

  tenantName.textContent = `${persona.nombre} ${persona.apellido}`;
  roleLabel.textContent = role === "inquilino"
    ? "Inquilino · Propiedad Av. Colón 1234, Depto 3"
    : "Propietario · Propiedad Av. Colón 1234, Depto 3";

  statusHeading.textContent = demo.heading;
  statusAmount.textContent = demo.amount;
  statusPeriod.textContent = demo.period;
  statusDue.innerHTML = demo.paid ? demo.paidLabel : demo.dueLabel;
  statusBadge.textContent = demo.paid ? "Pagado" : "Pendiente";
  statusBadge.className = `badge ${demo.paid ? "badge--paid" : "badge--pending"}`;

  historyHeading.textContent = demo.historyHeading;

  uploadTriggerBtn.hidden = role !== "inquilino";
  downloadTriggerBtn.hidden = role !== "propietario";
  reminderBtn.hidden = role !== "inquilino";

  demoNote.innerHTML = role === "inquilino"
    ? `Los montos e historial son datos de demostración: todavía no existe el modelo de Contrato/Pago en el backend. El acceso por DNI sí es real (consulta a <code>${endpoint}</code>).`
    : `Los montos, comisión e historial son datos de demostración: todavía no existe el modelo de Contrato/Liquidación en el backend. El acceso por DNI sí es real (consulta a <code>${endpoint}</code>).`;

  ledgerList.innerHTML = "";
  demo.ledger.forEach((entry, index) => {
    const row = document.createElement("li");
    row.className = "ledger__row";
    row.style.animationDelay = `${index * 40}ms`;
    row.innerHTML = `
      <span class="ledger__period">${entry.period}</span>
      <span class="ledger__meta">
        <span class="ledger__amount">${entry.amount}</span>
        <span class="badge ${entry.paid ? "badge--paid" : "badge--pending"}">${entry.paid ? "Pagado" : "Pendiente"}</span>
      </span>
    `;
    ledgerList.appendChild(row);
  });

  accessView.hidden = true;
  portalView.hidden = false;
}

logoutBtn.addEventListener("click", () => {
  portalView.hidden = true;
  accessView.hidden = false;
  dniInput.value = "";
  dniInput.focus();
});

reminderBtn.addEventListener("click", () => {
  showToast("Le avisamos a UBIKAR que querés un recordatorio por WhatsApp.");
});

downloadTriggerBtn.addEventListener("click", () => {
  showToast("Generando el PDF de la liquidación... (simulado, todavía no hay generación real de PDF)");
});

// --- Upload modal ---

let selectedFile = null;

uploadTriggerBtn.addEventListener("click", () => {
  uploadModal.hidden = false;
});

cancelUploadBtn.addEventListener("click", closeUploadModal);

uploadModal.addEventListener("click", (event) => {
  if (event.target === uploadModal) closeUploadModal();
});

function closeUploadModal() {
  uploadModal.hidden = true;
  selectedFile = null;
  fileInput.value = "";
  dropzoneLabel.textContent = "Arrastrá el archivo aquí o hacé clic para elegirlo";
  confirmUploadBtn.disabled = true;
}

fileInput.addEventListener("change", () => {
  if (fileInput.files.length > 0) {
    setSelectedFile(fileInput.files[0]);
  }
});

dropzone.addEventListener("dragover", (event) => {
  event.preventDefault();
  dropzone.classList.add("dropzone--active");
});

dropzone.addEventListener("dragleave", () => {
  dropzone.classList.remove("dropzone--active");
});

dropzone.addEventListener("drop", (event) => {
  event.preventDefault();
  dropzone.classList.remove("dropzone--active");
  if (event.dataTransfer.files.length > 0) {
    setSelectedFile(event.dataTransfer.files[0]);
  }
});

function setSelectedFile(file) {
  selectedFile = file;
  dropzoneLabel.textContent = file.name;
  confirmUploadBtn.disabled = false;
}

confirmUploadBtn.addEventListener("click", () => {
  closeUploadModal();
  showToast("Comprobante enviado. UBIKAR lo va a revisar a la brevedad.");
});

// --- Toast ---

let toastTimer = null;

function showToast(message) {
  toast.textContent = message;
  toast.hidden = false;
  requestAnimationFrame(() => toast.classList.add("toast--visible"));

  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toast.classList.remove("toast--visible");
    setTimeout(() => {
      toast.hidden = true;
    }, 220);
  }, 3200);
}
