customElements.define('%s',
  class extends HTMLElement {
    constructor() {
      super();
      let template = document.getElementById('%s');
      let templateContent = template.content;

      const shadowRoot = this.attachShadow({mode: 'open'})
        .appendChild(templateContent.cloneNode(true));
  }
})