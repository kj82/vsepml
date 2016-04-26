(function() {
  var app = angular.module('bigmlEditor',[]);
  
  app.controller('editor',['$scope','bigmlComponents', function(scope,components) {
    scope.jointGraph = new joint.dia.Graph();
    scope.componentsList = components;
    scope.propertyEditorElement = [1, 2];
  }]);
  
  app.directive('componentListElement', ['jointjsDragDrop', function(dragdrop) {
    return {
      template: '<img ng-if="component.icon" ng-src="{{ component.icon }}" alt="{{ component.text }}"> {{ component.text }}',
      link: function(scope, elem) {
        elem.attr('draggable','true');
        elem.on('dragstart', function(e) {
          e.originalEvent.dataTransfer.setData('bigmlComponent',scope.component.dragId);
          
          var icon = dragdrop.getIconElement(scope.component.dragId);
          if (icon) e.originalEvent.dataTransfer.setDragImage(icon.icon, icon.xOffset, icon.yOffset);
        });
        elem.css('cursor','pointer');
      }      
    };
  }]);
  
  app.directive('componentPropertiesEditor', function() {
    return {
      link: function(scope, elem) {
        // Add functions to editor scope
        scope.showProperties = function(jointEl) {
          scope.$apply(function() {
            scope.propertyEditorElement = jointEl;
          });
          elem.removeClass('stowed');
        };
        
        scope.hideProperties = function() {
          elem.addClass('stowed');
        };
      }
    };
  });
  
})();