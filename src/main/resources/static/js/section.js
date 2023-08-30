var originalFormData;

function selectTerm() {
  var divList = document.querySelectorAll('[id^="term-div-"]');
  divList.forEach(div => {
    div.style.display = 'none';
    div.style.visibility = 'hidden';
  });
  var selectElement = document.getElementById("term-select");
  var selectedTerm = selectElement.value;
  // -1 is the value for the 'Select Term' option
  if(selectedTerm == -1) {
    return;
  }
  var selectedTermDiv = document.getElementById('term-div-' + selectedTerm);
  selectedTermDiv.style.display = '';
  selectedTermDiv.style.visibility = 'visible';
}

function selectAllSectionsForWaitlists() {
  var checkAll = document.getElementById("selectAllSectionsForWaitlistsButton").checked;
  var inputList = document.querySelectorAll('[id^="waitlist-section-"]');
  inputList.forEach(input => {
    input.checked = checkAll;
  });
}

function selectAllSectionsForTerm(element) {
  var checked = element.checked;
  var parentDiv = element.parentNode;
  var inputList = parentDiv.querySelectorAll('input');
  inputList.forEach(input => {
    input.checked = checked;
  });
}

function disableButtons() {
  // Anchors don't support the disabled attribute, so we need to add a disabled class
  var anchorList = document.querySelectorAll("a");
  anchorList.forEach(anchor => {
    anchor.classList.add("disabled");
    anchor.setAttribute("aria-disabled", "true");
  });
  var buttonList = document.querySelectorAll("button");
  buttonList.forEach(button => {
    button.disabled = true;
  });
}

function showLoadingIcon(element) {
  const spinnerSpan = document.createElement('span');
  spinnerSpan.setAttribute("class", "spinner-border spinner-border-sm");
  spinnerSpan.setAttribute("role", "status");
  spinnerSpan.setAttribute("aria-hidden", "true");
  element.appendChild(spinnerSpan);
}

document.addEventListener("DOMContentLoaded", () => {
  // Anchor clicks don't fire a submit event, so we handle those manually
  var anchorList = document.querySelectorAll("a");
  anchorList.forEach(anchor => {
    anchor.onclick = function() {
      showLoadingIcon(anchor);
      disableButtons();
    };
  });
  var form = document.querySelector("form");
  form.addEventListener("submit", (event) => {
    showLoadingIcon(event.submitter);
    disableButtons();
  });
});