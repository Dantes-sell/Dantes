function sendRequest(event) {
  event.preventDefault();

  const name = document.getElementById("name").value.trim();
  const contact = document.getElementById("contactValue").value.trim();
  const status = document.getElementById("formStatus");

  if (!name || !contact) {
    status.textContent = "Заполни все поля.";
    return false;
  }

  status.textContent = "Заявка принята. Напиши менеджеру и укажи: " + name + ", " + contact;
  event.target.reset();
  return false;
}
