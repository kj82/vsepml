(function() {
  var bigml = joint.shapes.bigml = {};
  var mod = angular.module('bigmlEditor');
  
  mod.factory('bigmlComponents', function() {
    return {
      'root': [],
      'subgroups': []
    };
  });
  
  /* Component element */
  bigml.Component = joint.shapes.basic.Rect.extend({
    defaults: joint.util.deepSupplement({
      type: 'bigml.Component'
    }, joint.shapes.basic.Rect.prototype.defaults),
    properties: [
      { name: 'Name', type: 'string', default: 'hello' }
    ]
  });
    
  /* Data flow links */
  bigml.Dataflow = joint.dia.Link.extend({
    defaults: joint.util.deepSupplement({
        type: 'bigml.Dataflow',
        attrs: {
          '.marker-source': {
            d: ''
          },
          '.marker-target': {
            d: 'M 20 0 L 0 10 L 20 20 z'
          }
        }
    }, joint.dia.Link.prototype.defaults)
  });
  
  /* Composite element */
  bigml.Composite = bigml.Component.extend({
    defaults: joint.util.deepSupplement({
      type: 'bigml.Composite',
      size: { width: 400, height: 300 },
      attrs: {
        'rect': {
          rx: 10,
          ry: 10,
          'stroke-dasharray': '5,5'
        }
      }
    }, joint.shapes.basic.Rect.prototype.defaults)
  });
  
  /* Add to components list for GUI */
  mod.decorator('bigmlComponents', ['$delegate', function(components) {
    components.root.push({
      text: 'Composite',
      icon: '',
      jointEl: bigml.Composite
    });
    return components;
  }]);
})();