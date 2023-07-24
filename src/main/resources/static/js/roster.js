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

$(document).ready(function() {
  const spinnerSpan = document.createElement('span');
  spinnerSpan.setAttribute("class", "spinner-border spinner-border-sm");
  spinnerSpan.setAttribute("role", "status");
  spinnerSpan.setAttribute("aria-hidden", "true");
  var buttonList = document.querySelectorAll(".btn-primary");
  buttonList.forEach(button => {
    button.onclick = function() {
      // Once a button is clicked we want to disable all buttons found on the page, but only show the spinner on the clicked button.
      buttonList.forEach(button2 => {
        button2.disabled = true;
        button2.classList.add("disabled");
        button2.setAttribute("aria-disabled", "true");
      });
      button.appendChild(spinnerSpan);
    };
  });
});