#ifndef RETRORTS_PS1_CORE_H
#define RETRORTS_PS1_CORE_H

#include <string>

namespace retrorts::ps1 {

struct Ps1LaunchResult {
    bool ok;
    std::string message;
};

Ps1LaunchResult LaunchPs1Game(const std::string& romPath);

} // namespace retrorts::ps1

#endif // RETRORTS_PS1_CORE_H
