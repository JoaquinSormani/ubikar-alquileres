const API_URL = "http://localhost:8081/api/inquilinos";

function cargarInquilinos() {
  fetch(API_URL)
    .then(response => response.json())
    .then(inquilinos => {
      const lista = document.getElementById("listaInquilinos");
      lista.innerHTML = "";
      inquilinos.forEach(i => {
        const item = document.createElement("li");
        item.textContent = `#${i.id} - ${i.nombre} ${i.apellido} (DNI ${i.dni})`;
        lista.appendChild(item);
      });
    })
    .catch(error => console.error("Error al cargar inquilinos:", error));
}

function crearInquilino() {
  const inquilino = {
    nombre: document.getElementById("nombre").value,
    apellido: document.getElementById("apellido").value,
    dni: document.getElementById("dni").value,
    telefono: document.getElementById("telefono").value,
    email: document.getElementById("email").value
  };

  fetch(API_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(inquilino)
  })
    .then(response => response.json())
    .then(() => cargarInquilinos())
    .catch(error => console.error("Error al crear inquilino:", error));
}

cargarInquilinos();
