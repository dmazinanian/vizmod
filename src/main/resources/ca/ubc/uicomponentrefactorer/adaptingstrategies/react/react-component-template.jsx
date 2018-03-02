class ${componentClassName} extends React.Component {
    render() {
        return(
            ${componentBody}
        );
    }
}

//export default ${componentClassName};

let dynamicProps = [${componentParameterizedTrees}]

  for (let i = 0; i < dynamicProps.length; i++) {
    ReactDOM.render(
      <ReactComponent {...dynamicProps[i]} />,
      document.getElementById('${componentClassName}' + i)
    );
  }