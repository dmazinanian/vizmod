class ${componentClassName} extends React.Component {
    render() {
        return(${componentBody});
    }
}

//export default ${componentClassName};

let dynamicProps${componentClassName} = [${componentParameterizedTrees}]

for (let i = 0; i < dynamicProps${componentClassName}.length; i++) {
    ReactDOM.render(
        <${componentClassName} {...dynamicProps${componentClassName}[i]} />,
        document.getElementById('${componentClassName}' + i)
    );
}

// Replace custom elements with original root nodes
for (let i = 0; i < dynamicProps${componentClassName}.length; i++) {
    let customElement = document.getElementById('${componentClassName}' + i);
    customElement.parentNode.replaceChild(customElement.childNodes[0], customElement);
}