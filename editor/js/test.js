var graph = new joint.dia.Graph();
var paper = new joint.dia.Paper({
    el: $('#paper'),
    width: 800,
    height: 600,
    gridSize: 1,
    model: graph,
    snapLinks: true,
    linkPinning: false,
    embeddingMode: true
});

paper.on('blank:pointerdown', function(eventName, cell) {
    //search for the sidebar active
    var active=$(".list-group-item.active").get(0);

    if(active != null){
        elementFabric(active.textContent,arguments);
    }else{
        console.log("Error No element selected");
    }
    $(".list-group-item").removeClass("active");
});

function elementFabric(elementTypeName,arguments){
    var f;
    switch (elementTypeName) {
            case "Fork": 
                f=addFork(arguments[1], arguments[2]);
                break;
            case "Join":
                f=addJoin(arguments[1], arguments[2]);
                break;
            case "Generic":
                f=addSource(arguments[1], arguments[2]);
                break;
            case "Database":
                f=addDatastore(arguments[1], arguments[2], "Generic datastore");
                break;
            case "Composite":
                f=addComposite(arguments[1], arguments[2], "Composite");
                break;
            default:
                break;
        }
        return f;
}

function addFork(x,y){
    var fork = new joint.shapes.pn.Transition({
        position: { x: x, y: y },
        attrs: {
            '.label': { text: 'fork', fill: '#fe854f' },
            '.root' : { fill: 'black', stroke: 'black' }
        }
    });
    graph.addCell(fork);
    return fork;
}

function addJoin(x,y){
    var join = new joint.shapes.pn.Transition({
        position: { x: x, y: y },
        attrs: {
            '.label': { text: 'join', fill: '#fe854f' },
            '.root' : { fill: 'black', stroke: 'black' }
        }
    });
    graph.addCell(join);
    return join;
}

function addDatastore(x,y,name){
    var store = new joint.shapes.devs.Model({
        position: { x: x, y: y },
        size: { width: 140, height: 90 },
        inPorts: ['in1','in2'],
        outPorts: ['out'],
        attrs: {
            '.label': { text: name, 'ref-x': .4, 'ref-y': .2 },
            rect: { fill: 'orange' },
            '.inPorts circle': { fill: '#16A085' },
            '.outPorts circle': { fill: '#E74C3C' },
            '.body': { 'rx': 6, 'ry': 6 }
        }
    });
    graph.addCell(store); 
    return store;  
}

function addComposite(x,y,name){
    var composite = new joint.shapes.devs.Model({
        position: { x: x, y: y },
        size: { width: 140, height: 140 },
        attrs: {
            '.label': { text: name, 'ref-x': .4, 'ref-y': .2 },
            rect: { fill: 'white' },
            '.inPorts circle': { fill: '#16A085' },
            '.outPorts circle': { fill: '#E74C3C' },
            '.body': { 'rx': 6, 'ry': 6 }
        }
    });
    graph.addCell(composite);  
}

function addSource(x,y){
    var source = new joint.shapes.pn.Place({
        position: { x: x, y: y },
        attrs: {
            '.label': { text: 'Data Source', fill: '#7c68fc' },
            '.root' : { stroke: '#9586fd', 'stroke-width': 3 },
            '.tokens > circle': { fill : '#7a7e9b' }
        },
        tokens: 0
    });

    graph.addCell(source);
    return source;
}


