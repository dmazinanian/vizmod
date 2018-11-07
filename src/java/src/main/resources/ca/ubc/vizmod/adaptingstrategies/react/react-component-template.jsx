class ${componentClassName} extends React.Component{
    render(){return(${componentBody});}
}
let props${componentClassName}=[${componentParameterizedTrees}];
for(let i=0; i < props${componentClassName}.length; i++) {
    let ce=document.getElementById('${componentClassName}'+i);
    ReactDOM.render(<${componentClassName} {...props${componentClassName}[i]} />,ce);
    ce.parentNode.replaceChild(ce.childNodes[0],ce);
}
