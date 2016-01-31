const gulp = require('gulp');
var del = require('del');
const babel = require('gulp-babel');
var browserify = require('browserify');
var source = require('vinyl-source-stream');
var babelify = require('babelify');
var uglify = require('gulp-uglify');
var watchify = require('watchify');
var streamify = require('gulp-streamify');
var rename = require('gulp-rename');

var path = {
  HTML: 'src/index.html',
  ALL: ['src/js/*.js', 'src/js/**/*.js', 'src/index.html'],
  JS: ['src/js/*.js', 'src/js/**/*.js'],
  OUT: 'build.js',
  MINIFIED_OUT: 'build.min.js',
  DEST: 'dist',
  DEST_SRC: 'dist/src',
  DEST_BUILD: 'dist/build',
  ENTRY_POINT: './src/js/components/stamina-app.js'
};

// Transforms JSX/ES2015 into ES5
gulp.task('transform', function() {
  return gulp.src(path.JS)
      .pipe(babel({
        presets: ['es2015', 'react']
      }))
      .pipe(gulp.dest(path.DEST_SRC));
});

gulp.task('build', function(){
  browserify({
    entries: [path.ENTRY_POINT],
    transform: [['babelify', {'presets': ['es2015', 'react']}]],
  })
    .bundle()
    .pipe(source(path.MINIFIED_OUT))
    .pipe(streamify(uglify()))
    .pipe(gulp.dest(path.DEST_BUILD));
});

gulp.task('watch', function() {

  gulp.watch(path.HTML, ['copy']);

  var watcher  = watchify(browserify({
    entries: [path.ENTRY_POINT],
    transform: [['babelify', {'presets': ['es2015', 'react']}]],
    debug: true,
    cache: {}, packageCache: {}, fullPaths: true
  }));

  return watcher.on('update', function () {
    watcher.bundle()
      .pipe(source(path.OUT))
      .pipe(gulp.dest(path.DEST_SRC))
      console.log('Updated');
  })
    .bundle()
    .pipe(source(path.OUT))
    .pipe(gulp.dest(path.DEST_SRC));
});

gulp.task('build:copy:fonts', ['del:dist'], function() {
	gulp.src('./src/fonts/*.*')
		.pipe(gulp.dest('./dist/fonts'));
});

gulp.task('build:browserify', ['del:dist'], function() {
	var bundler = browserify('./src/js/components/stamina-app.js');
	bundler.transform(babelify);

	return bundler.bundle()
		.pipe(source('stamina.js'))
		.pipe(gulp.dest('./dist/js'));
});

// SIMPLE
gulp.task('copy:index', function() {
  gulp.src(path.HTML)
    .pipe(gulp.dest(path.DEST));
});

gulp.task('browserify', function() {
	var bundler = browserify('./src/js/components/stamina-app.js');
	bundler.transform(babelify);

	return bundler.bundle()
		.pipe(source('stamina.js'))
		.pipe(gulp.dest('./dist/js'));
});
