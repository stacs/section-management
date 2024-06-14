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
  element.textContent=strings['button.loading'];
  const spinnerSpan = document.createElement('span');
  spinnerSpan.setAttribute("class", "spinner-border spinner-border-sm");
  spinnerSpan.setAttribute("role", "status");
  spinnerSpan.setAttribute("aria-hidden", "true");
  element.appendChild(spinnerSpan);
  document.getElementById("sr-text").textContent=strings['button.loading.srText'];
}

function termCheck() {
  var courseTermName = document.getElementById("course-term-name").value;
  var rostersToCrosslist = document.querySelectorAll("input:checked[data-term]");
  var differentTerms = new Set();
  rostersToCrosslist.forEach(function(rosterElement) {
    var term = rosterElement.dataset.term;
    if (term !== courseTermName) {
      differentTerms.add(term);
    }
  });
  if(differentTerms.size === 0) {
    return true;
  } else {
    var text = strings['alert.multipleTerms.text'];
    message = stringInterpolation(text, differentTerms.values().next().value, courseTermName);
    Swal.fire({
      title: strings['alert.multipleTerms.title'],
      html: message,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: strings['alert.multipleTerms.confirmText']
    }).then((result) => {
      return result.isConfirmed;
    });
  }
}

// While not perfect, this is an attempt to replicate the string interpolation that thymeleaf uses so we don't
// have to worry about different message formats depending on whether the message came from Java or Javascript
function stringInterpolation(message, ...vars) {
  for(var i=0; i<vars.length; i++) {
    message = message.replace(`{${i}}`, vars[i]);
  }
  return message;
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
  // TODO: split javascript into one file per page
  if(form.id === "sectionManagementForm") {
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      if(termCheck()) {
        showLoadingIcon(event.submitter);
        disableButtons();
        form.submit();
      }
    });
  } else {
    form.addEventListener("submit", (event) => {
      showLoadingIcon(event.submitter);
      disableButtons();
    });
  }
});