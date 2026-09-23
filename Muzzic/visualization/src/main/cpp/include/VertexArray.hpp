#pragma once

#include "Renderer/OpenGL.h"

namespace libprojectM {
namespace Renderer {

/**
 * @brief Wraps a vertex array object.
 * Creates, destroys and binds a single VAO, tracking active bindings to avoid no-op state changes.
 */
class VertexArray
{
public:
    /**
     * Constructor. Creates a new VAO.
     */
    VertexArray()
    {
        glGenVertexArrays(1, &m_vaoID);
    }

    /**
     * Destructor. Deletes the stored VAO.
     */
    virtual ~VertexArray()
    {
        if (s_currentBoundVao == m_vaoID) {
            s_currentBoundVao = 0;
        }
        glDeleteVertexArrays(1, &m_vaoID);
        m_vaoID = 0;
    }

    /**
     * Binds the stored VAO if not already bound.
     */
    void Bind() const
    {
        if (s_currentBoundVao != m_vaoID) {
            glBindVertexArray(m_vaoID);
            s_currentBoundVao = m_vaoID;
        }
    }

    /**
     * Binds the default VAO with ID 0.
     */
    static void Unbind()
    {
        if (s_currentBoundVao != 0) {
            glBindVertexArray(0);
            s_currentBoundVao = 0;
        }
    }

private:
    GLuint m_vaoID{0}; //!< The vertex array object ID for this mesh's vertex data.
    inline static GLuint s_currentBoundVao{0};
};

} // namespace Renderer
} // namespace libprojectM
