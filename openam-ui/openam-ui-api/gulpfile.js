/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.1.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.1.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2024 Wren Security.
 */

const {
  useLocalResources,
} = require("@wrensecurity/commons-ui-build");
const gulp = require("gulp");
const { dirname } = require("path");

const TARGET_PATH = "target/www";
const DEPLOY_DIR = `${process.env.OPENAM_HOME}/api`;

gulp.task("build:assets", useLocalResources({ "src/main/resources/**": "" }, { dest: TARGET_PATH }));

gulp.task("build:swagger", () => {
  const baseDir = dirname(require.resolve("swagger-ui-dist/swagger-ui-bundle.js"));
  return gulp.src([
      `${baseDir}/swagger-ui-bundle.{js,js.map}`,
      `${baseDir}/swagger-ui-standalone-preset.{js,js.map}`,
      `${baseDir}/*.css`,
      `${baseDir}/*.{png,gif,jpg,ico,svg,ttf,eot,woff}`,
  ], { base: baseDir, encoding: false }).pipe(gulp.dest(TARGET_PATH));
});

gulp.task("build", gulp.parallel(
  "build:assets",
  "build:swagger",
));

gulp.task("deploy", () => gulp.src(`${TARGET_PATH}/**/*`, { encoding: false }).pipe(gulp.dest(DEPLOY_DIR)));

gulp.task("dev", gulp.series("build", "deploy"));
gulp.task("prod", gulp.series("build"));

gulp.task("default", gulp.series("dev"));
