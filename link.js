// `react-native link-kotlin`
// implementation auto link native modules written on Kotlin language
//  WORK IN PROGRESS

/**
 * getPackageBaseClass
 * @param {string} file - string with source kotlin file
 * @return {string} - result package class name for inject 
 */

const getPackageBaseClass = (file) => {
  let lines = file.trim().split('\n')
  let result = null

  let regexes = {
    ext: /(.*)ReactPackage/,
    name: /^class (.*)/
  }

  for (let index = 0; index < lines.length; index++) {
    let nextLine = lines[index + 1] ? lines[index + 1].trim() : ''
    let line = lines[index].trim()

    let valid = {
      name: regexes.name.test(line),
      ext: [
        regexes.ext.test(nextLine),
        regexes.ext.test(line)
      ].some(b=>b)
    }

    let matcher = line.match(regexes.name)
    if (valid.name && valid.ext) {
      result = matcher[1].split(' ')[0]
      break
    }
  }

  return result
}